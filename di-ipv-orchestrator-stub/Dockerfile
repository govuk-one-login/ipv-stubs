FROM openjdk:17-jdk-slim AS build
COPY . /home/gradle
WORKDIR /home/gradle
RUN ./gradlew build --no-daemon --stacktrace


FROM openjdk:17-jdk-slim

ENV PORT 8083
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007
RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 appuser --gid 1001
RUN mkdir /app
RUN apt-get update && apt-get install -y curl
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
