FROM amazoncorretto:17-alpine3.17-jdk AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build --no-daemon

FROM amazoncorretto:17-alpine3.17-jdk

ENV PORT 8084
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5006
RUN addgroup --system -g 1001 appgroup
RUN adduser --system -u 1001 appuser -G appgroup
RUN mkdir /app
RUN apk update && apk --no-cache add curl
COPY --from=build \
    /home/gradle/src/build/distributions/di-ipv-credential-issuer-stub.tar \
    /app/

WORKDIR /app

RUN tar -xvf di-ipv-credential-issuer-stub.tar \
    && rm di-ipv-credential-issuer-stub.tar

RUN chown -R appuser:appgroup /app/
USER appuser
EXPOSE $PORT
EXPOSE 5006

ENTRYPOINT ["./di-ipv-credential-issuer-stub/bin/di-ipv-credential-issuer-stub"]
LABEL project="di-ipv-credential-issuer-stub"
