import os
import sys
import zipfile

files = [f for f in os.listdir(".") if f.endswith(".so") or f.endswith(".dll")]

print(f"write zip file {sys.argv[1]}")

with zipfile.ZipFile(sys.argv[1], "w") as zip:
    zip.write("native.txt")
    for f in files:
        zip.write(f)