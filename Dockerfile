FROM JAVA:7
COPY config-wrapper 
WORKDIR config-wrapper 
RUN ./gradlwe installDist
WORKDIR build/install/config-wrapper
CMD ["bin/config-wrapper"]