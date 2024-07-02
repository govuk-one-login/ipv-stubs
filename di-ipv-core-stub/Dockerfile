FROM amazoncorretto:17-alpine3.17-jdk AS build

WORKDIR /home/gradle/src
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
RUN ./gradlew build --no-daemon
COPY src src
RUN ./gradlew build --no-daemon

FROM amazoncorretto:17-alpine3.17-jdk

ENV PORT 8085
ENV DEBUG_PORT 8087
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORT
RUN addgroup --system -g 1001 appgroup
RUN adduser --system -u 1001 appuser -G appgroup
RUN mkdir /app
RUN apk update && apk --no-cache add curl
COPY --from=build \
    /home/gradle/src/build/distributions/src.tar \
    /app/

WORKDIR /app
ADD config config

RUN tar -xvf src.tar \
    && rm src.tar

RUN chown -R appuser:appgroup /app/
USER appuser

# Add in dynatrace layer
COPY --from=khw46367.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

EXPOSE $PORT
EXPOSE $DEBUG_PORT

ENTRYPOINT ["./src/bin/src"]
LABEL project="di-ipv-core-stub"
