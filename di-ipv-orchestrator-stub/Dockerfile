FROM amazoncorretto:21.0.13-al2@sha256:95ff45e69538515d9f55dd43a1c6e46d1ecebf63faa38a4fdd303f3b3e37ccba AS build
COPY . /home/gradle
WORKDIR /home/gradle
RUN ./gradlew build --no-daemon --stacktrace


FROM amazoncorretto:21.0.13-al2@sha256:95ff45e69538515d9f55dd43a1c6e46d1ecebf63faa38a4fdd303f3b3e37ccba

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
