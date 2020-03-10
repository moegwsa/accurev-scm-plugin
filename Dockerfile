FROM maven:3-jdk-8-slim AS build

USER root
ENV ACCUREV_HOME /var/AccurevClient
ENV PATH $PATH:${ACCUREV_HOME}/bin

RUN apt-get update && apt-get upgrade -y && \
  apt-get install -y curl unzip && \
  rm -rf /var/lib/apt/lists/*

COPY installer.properties .

RUN curl -fsSL http://cdn.microfocus.com/cached/legacymf/Products/accurev/accurev7.3/accurev-7.3-linux-x86-x64-clientonly.bin -o ./accurev-client.bin && \
  chmod +x ./accurev-client.bin

RUN apt-get update && \
    apt-get -y install apt-transport-https \
         ca-certificates \
         curl \
         gnupg2 \
         software-properties-common && \
    curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg > /tmp/dkey; apt-key add /tmp/dkey && \
    add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
       $(lsb_release -cs) \
       stable" && \
    apt-get update && \
    apt-get -y install docker-ce

COPY . /home/app/
COPY pom.xml /home/app

RUN ./accurev-client.bin -i silent -f ./installer.properties; exit 0

RUN mvn -f /home/app/pom.xml clean package
