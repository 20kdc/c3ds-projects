FROM --platform=linux/386 c3ds-projects-cie:c3ds
RUN ["rm", "-rf", "Bootstrap/000 Switcher"]
RUN ["mkdir", "Bootstrap/000 Switcher", "Bootstrap/100 Aquarium"]
ADD ["switcher.cos", "Bootstrap/000 Switcher/"]
ADD ["zzz_aquarium.cos", "Bootstrap/100 Aquarium/"]
ADD ["aquarium-looper", "aquarium-looper"]
VOLUME ["/c2e/Docking Station/Users", "/c2e/Docking Station/My Worlds", "/c2e/Docking Station/My Creatures", "/c2e/Docking Station/Journal"]
ENTRYPOINT ["/c2e/Docking Station/aquarium-looper"]
