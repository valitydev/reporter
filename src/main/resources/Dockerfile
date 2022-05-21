FROM openjdk:19-jdk-slim

RUN apt-get update \
      && apt-get install -y \
      --no-install-recommends \
      fontconfig=2.13.* \
      && rm -rf /var/lib/apt/lists/*

RUN useradd -s /bin/false -U -u 20000 vality

COPY --chown=vality:vality ${artifactId}-${version}.jar /opt/${artifactId}/${artifactId}.jar

USER vality:vality

CMD ["java", "-jar","/opt/${artifactId}/${artifactId}.jar"]

EXPOSE ${exposed.ports}

WORKDIR /opt/${artifactId}
