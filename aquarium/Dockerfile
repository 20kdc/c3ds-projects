FROM --platform=linux/386 c3ds-projects-cie:c3ds

# create /data volume for persistent state
RUN ["mkdir", "data", "data/Users", "data/My Worlds", "data/My Creatures", "data/Journal"]
VOLUME ["/data"]

# actually run install
RUN ["rm", "-rf", "Bootstrap/000 Switcher"]
RUN ["mkdir", "Bootstrap/000 Switcher", "Bootstrap/100 Aquarium"]
ADD ["switcher.cos", "Bootstrap/000 Switcher/"]
ADD ["zzz_aquarium.cos", "Bootstrap/100 Aquarium/"]
ADD ["aquarium-looper", "aquarium-looper"]

# and we're "done"?
ENTRYPOINT ["/c2e/Docking Station/aquarium-looper"]
