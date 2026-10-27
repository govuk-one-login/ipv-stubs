FROM amazoncorretto:21.0.13-al2@sha256:5d282fd62a4c14c3f7e045a9f9f7cbd5cf77b2f87dc10f08d851fdd30e1e474d AS build
COPY . /home/gradle
WORKDIR /home/gradle
RUN ./gradlew build --no-daemon --stacktrace


FROM amazoncorretto:21.0.13-al2@sha256:5d282fd62a4c14c3f7e045a9f9f7cbd5cf77b2f87dc10f08d851fdd30e1e474d

ENV PORT 8083
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007
RUN yum update -y && yum install -y shadow-utils curl tar
RUN groupadd --system --gid 1001 appgroup && useradd --system --uid 1001 appuser --gid 1001
RUN mkdir /app
COPY --from=build \
    /home/gradle/build/distributions/di-ipv-orchestrator-stub.tar \
    /app/

WORKDIR /app
RUN tar -xvf di-ipv-orchestrator-stub.tar \
    && rm di-ipv-orchestrator-stub.tar

RUN chown -R appuser:appgroup /app/
USER appuser

# Add in dynatrace layer
COPY --from=khw46367.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE $PORT
EXPOSE 5007

ENTRYPOINT ["./di-ipv-orchestrator-stub/bin/di-ipv-orchestrator-stub"]
LABEL project="di-ipv-orchestrator-stub"
