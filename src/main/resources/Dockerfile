# @dockerfile.Template@
FROM @dockerfile.registry@/rbkmoney/service-java:@dockerfile.base.service.tag@

RUN apt-get update \
      && apt-get install -y \
      fontconfig \
      && rm -rf /var/lib/apt/lists/*

COPY @artifactId@-@version@.jar /opt/@artifactId@/@artifactId@.jar

WORKDIR /opt/@artifactId@
CMD ["java", "-Xmx256m", "-jar","/opt/@artifactId@/@artifactId@.jar"]

EXPOSE @exposed.ports@

MAINTAINER @project.maintainer@
LABEL com.rbkmoney.@artifactId@.parent=service-java \
    com.rbkmoney.@artifactId@.parent_tag=@dockerfile.base.service.tag@ \
    com.rbkmoney.@artifactId@.commit_id=@git.commit.id@ \
    com.rbkmoney.@artifactId@.branch=@git.branch@
