FROM --platform=linux/386 c3ds-projects-cie:c2e
ADD repo/pkg_dockingstation.tar ./c2e/
RUN ["ln", "-s", "/proc/self/fd/1", "/c2e/Docking Station/Journal/stdout"]
RUN ["ln", "-s", "/proc/self/fd/2", "/c2e/Docking Station/Journal/stderr"]
RUN ["sh", "-c", "echo \"PortSecurity 0\" >> \"/c2e/Docking Station/user.cfg\""]
WORKDIR "/c2e/Docking Station"
ENTRYPOINT ["../engine/run-game", "--headless"]
