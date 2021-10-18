FROM gradle:7.2.0-jdk17@sha256:0619ec0c28cdd0f5f7ba389e7fea845b400cf282dd28b46b4d67f60a009732fa AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:17-jdk-slim@sha256:919c92bf200dc4faf73dcd858ac7a495edfafe7552e3a7ad6266c386a34bad60

ENV PORT 8081
RUN addgroup --system --gid 1001 appgroup && adduser --system --uid 1001 appuser --gid 1001
RUN mkdir /app
RUN apt-get update && apt-get install -y curl
COPY --from=build \
	/home/gradle/src/build/distributions/src.tar \
	/app/

WORKDIR /app
RUN tar -xvf src.tar \
	&& rm src.tar

RUN chown -R appuser:appgroup /app/
USER appuser
EXPOSE $PORT

ENTRYPOINT ["./src/bin/src"]
LABEL project="di-ipv-orchestrator-stub"