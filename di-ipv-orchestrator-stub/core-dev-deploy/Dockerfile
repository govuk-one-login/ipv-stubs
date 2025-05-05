FROM amazoncorretto:21.0.6-al2@sha256:c0643148d077b3917bf6c085aaa1ac35ff1f202344ab8691cc0f5953f9b97f8e AS build
COPY . /home/gradle
WORKDIR /home/gradle
RUN ./gradlew build --no-daemon --stacktrace


FROM amazoncorretto:21.0.6-al2@sha256:c0643148d077b3917bf6c085aaa1ac35ff1f202344ab8691cc0f5953f9b97f8e

ENV PORT 8083
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007
RUN yum update && yum install -y shadow-utils curl tar
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
EXPOSE $PORT
EXPOSE 5007

ENTRYPOINT ["./di-ipv-orchestrator-stub/bin/di-ipv-orchestrator-stub"]
LABEL project="di-ipv-orchestrator-stub"
