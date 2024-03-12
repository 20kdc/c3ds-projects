FROM --platform=linux/386 c3ds-projects-cie:ds
WORKDIR "/"
ADD repo/pkg_creatures3.tar ./c2e/
WORKDIR "/c2e/Docking Station"
