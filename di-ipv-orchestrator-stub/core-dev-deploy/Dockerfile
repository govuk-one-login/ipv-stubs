FROM amazoncorretto:17-alpine3.17-jdk AS build
COPY . /home/gradle
WORKDIR /home/gradle
RUN ./gradlew build --no-daemon --stacktrace


FROM amazoncorretto:17-alpine3.17-jdk

ENV PORT 8083
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007
RUN addgroup --system -g 1001 appgroup
RUN adduser --system -u 1001 appuser -G appgroup
RUN mkdir /app
RUN apk update && apk --no-cache add curl
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
