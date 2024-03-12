FROM --platform=linux/386 c3ds-projects-cie:c2e
ADD repo/pkg_dockingstation.tar ./c2e/
WORKDIR "/c2e/Docking Station"
ENTRYPOINT ["../engine/run-game-headless"]
