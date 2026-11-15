"""Inspeciona recursos do JAR sem executar a API nem mostrar conteúdo de arquivos."""
import argparse
from pathlib import Path
import sys
import zipfile


def verify(jar_path, marker=None):
    resources = Path(__file__).resolve().parents[1] / "src/main/resources"
    expected = {"application.yaml", "application-dev.yaml", "application-prod.yaml",
                "email/logo-mark.png"}
    expected.update(path.relative_to(resources).as_posix()
                    for path in (resources / "db/migration").rglob("*.sql"))
    prefix = "BOOT-INF/classes/"
    with zipfile.ZipFile(jar_path) as jar:
        names = set(jar.namelist())
        actual = {name.removeprefix(prefix) for name in names
                  if name.startswith(prefix) and not name.endswith(("/", ".class"))}
        if actual != expected:
            # Mostrar apenas contagens: um nome inesperado também pode ser privado.
            raise ValueError(f"Recursos divergentes: ausentes={len(expected - actual)}, "
                             f"inesperados={len(actual - expected)}")
        if marker:
            sentinel = marker.encode("utf-8")
            for name in names:
                if not name.endswith("/") and sentinel in jar.read(name):
                    raise ValueError("Marcador fictício encontrado no artefato.")
    print(f"JAR aprovado: {len(expected)} recursos esperados; nenhum recurso extra"
          + (" ou marcador fictício." if marker else "."))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path)
    parser.add_argument("--marker", help="Somente marcador fictício, nunca uma credencial real.")
    args = parser.parse_args()
    try:
        verify(args.jar, args.marker)
    except (ValueError, OSError, zipfile.BadZipFile):
        print("Inspeção do JAR reprovada; nenhum conteúdo foi exibido.", file=sys.stderr)
        sys.exit(1)
