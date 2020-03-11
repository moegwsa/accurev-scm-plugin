#!/bin/bash
curl -fsSL http://cdn.microfocus.com/cached/legacymf/Products/accurev/accurev7.3/accurev-7.3-linux-x86-x64-clientonly.bin -o ./accurev-client.bin
chmod +x ./accurev-client.bin
./accurev-client.bin -i silent -f ./installer.properties || true
