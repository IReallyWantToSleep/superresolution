#!/bin/python

import subprocess
import sys

def get_path(argv:str):
    if argv.startswith("/mnt/"):
        return ("",argv)
    path:str = argv[2:len(argv)]
    if path.startswith("/mnt/"):
        return (argv[0:2],path)
    path = argv.split("=")[-1]
    if path.startswith("/mnt/"):
        return (argv.split("=")[0]+"=",path)

if __name__ == '__main__':
    argv  = sys.argv
    argv.remove(argv[0])
    print(argv)
    for s in argv:
        if get_path(s) != None:
            a = get_path(s)
            path = a[1]
            de = path.split("/")[2].upper()
            out = f'{a[0]}{de}:/{"/".join(path.split("/")[3:999999])}'
            argv[argv.index(s)] = out
    subprocess.call(["/mnt/n/fsr2_win64/tools/FidelityFX_SC.exe"]+argv)

