import os
import shutil

files = [f for f in os.listdir(".") if f.endswith(".so") or f.endswith(".dll")]

BUF_SIZE=65535
with open("native.txt","w") as nativeTxt:
    for f in files:
        nativeTxt.write(f"{f}\n")
        shutil.copyfile(f, f"src/main/resources/{f}")
        print(f"handled {f}")


shutil.copyfile("native.txt", "src/main/resources/native.txt")

