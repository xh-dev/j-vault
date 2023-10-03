import xml.etree.ElementTree as ET
import datetime as dt
from zoneinfo import ZoneInfo
hk = ZoneInfo('Asia/Hong_Kong')

time_str=dt.datetime.now(tz=hk).strftime("%Y-%m-%d %H-%M")
ns_map = {
  'e': 'http://maven.apache.org/POM/4.0.0'
}

tree = ET.parse('pom.xml')
version=tree.getroot().find('e:version', ns_map).text
v=""
v+=f"version: {version}\n"
v+=f"build: {time_str}"
print(v)