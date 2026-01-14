#!/usr/bin/env python3
import argparse
import importlib
import importlib.util
import json
import sys
import tempfile
from pathlib import Path

from google.protobuf import json_format


def load_module_from_path(module_name, module_path):
    spec = importlib.util.spec_from_file_location(module_name, module_path)
    if spec is None or spec.loader is None:
        return None
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def try_import_module(module_name, proto_path, gen_dir):
    try:
        return importlib.import_module(module_name)
    except ModuleNotFoundError:
        pass

    if proto_path is None:
        return None

    try:
        from grpc_tools import protoc
    except ModuleNotFoundError:
        return None

    proto_path = Path(proto_path)
    if not proto_path.exists():
        return None

    gen_dir = Path(gen_dir)
    gen_dir.mkdir(parents=True, exist_ok=True)

    args = [
        "protoc",
        f"-I{proto_path.parent}",
        f"--python_out={gen_dir}",
        str(proto_path),
    ]
    result = protoc.main(args)
    if result != 0:
        return None

    sys.path.insert(0, str(gen_dir))
    try:
        return importlib.import_module(module_name)
    except ModuleNotFoundError:
        module_path = gen_dir / f"{module_name}.py"
        if module_path.exists():
            return load_module_from_path(module_name, module_path)
        return None


def main():
    parser = argparse.ArgumentParser(description="Convert .flarereport protobuf+zstd to JSON.")
    parser.add_argument("input", help="Path to .flarereport")
    parser.add_argument("output", help="Path to output JSON")
    parser.add_argument(
        "--proto",
        default=str(Path("src/main/proto/flare_report.proto")),
        help="Path to flare_report.proto (used for auto-generation)",
    )
    parser.add_argument(
        "--module",
        default="flare_report_pb2",
        help="Python protobuf module name (default: flare_report_pb2)",
    )
    parser.add_argument(
        "--gen-dir",
        default=None,
        help="Directory to place generated protobuf code (defaults to temp dir)",
    )
    args = parser.parse_args()

    try:
        import zstandard as zstd
    except ModuleNotFoundError:
        print("Missing dependency: zstandard. Install with: pip install zstandard", file=sys.stderr)
        return 1

    gen_dir = args.gen_dir
    temp_dir = None
    if gen_dir is None:
        temp_dir = tempfile.TemporaryDirectory()
        gen_dir = temp_dir.name

    module = try_import_module(args.module, args.proto, gen_dir)
    if module is None:
        print("Unable to import protobuf module.", file=sys.stderr)
        print("Ensure you installed grpcio-tools in the same Python used to run this script.", file=sys.stderr)
        print("You can also generate manually with:", file=sys.stderr)
        print("  python -m grpc_tools.protoc -I src/main/proto --python_out . src/main/proto/flare_report.proto", file=sys.stderr)
        return 1

    input_path = Path(args.input)
    output_path = Path(args.output)

    compressed = input_path.read_bytes()
    decompressed = zstd.ZstdDecompressor().decompress(compressed)

    message = module.ProfilerData()
    message.ParseFromString(decompressed)

    data = json_format.MessageToDict(message, preserving_proto_field_name=True)
    output_path.write_text(json.dumps(data, indent=2))

    if temp_dir is not None:
        temp_dir.cleanup()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
