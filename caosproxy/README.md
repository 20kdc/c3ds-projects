# CAOSProxy

CAOSProxy, or CPX, is a network-transparent protocol to connect to the C2E shared memory interface described at http://double.nz/creatures/developer/sharedmemory.htm .

It is intended for mixed environments where the game or applications may not be running natively.

For further information see `spec.txt`.

## CAOSProxy Server W32

This is a CAOSProxy (CPX) server to be used on Wine or on Windows.

```
caosprox [loud/quiet/invisible/help] [HOST] [PORT] [game name]
default mode is loud, HOST is 127.0.0.1 (set to 0.0.0.0 for remote access), default PORT is 19960, default game is autodetected
all prior args must be specified if you wish to use one
```

