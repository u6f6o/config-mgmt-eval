FROM java:7
ADD config-wrapper config-wrapper/  
WORKDIR config-wrapper 
RUN ./gradlew installDist
WORKDIR build/install/config-wrapper
CMD ["bin/config-wrapper"]
