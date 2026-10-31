"""Read-only provenance check. Archive content is data, never executed."""
import hashlib
import pathlib
import re
import zipfile

archive = pathlib.Path('D:/cinestream-tv---android-tv-compose-app-&-simulator.zip')
with zipfile.ZipFile(archive) as z:
    source = z.read('src/data/androidKotlinCodebase.ts')
    text = source.decode()
    print('Archive SHA256:', hashlib.sha256(archive.read_bytes()).hexdigest())
    print('Embedded file definitions:', len(re.findall(r"path: '", text)))
    print('Web source matches archive:', source == pathlib.Path('D:/CineStreamTV/src/data/androidKotlinCodebase.ts').read_bytes())
    for name in ('TvAppNavigation', 'MockData', 'CineStreamApp'):
        print(name, 'defined:', bool(re.search(r'(class|object|fun) ' + name, text)))
    print('Exporter generates omitted resources:', 'res/values/strings.xml' in z.read('src/utils/zipExporter.ts').decode())
