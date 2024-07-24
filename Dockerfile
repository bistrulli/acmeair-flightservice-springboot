FROM ubuntu:22.04

RUN apt-get update -y
RUN apt-get install openssh-server git openjdk-17-jdk maven redis curl iputils-ping htop -y

WORKDIR /root
COPY . /root/acmeair-flightservice-springboot
RUN git clone https://github.com/bistrulli/acmeair-ctrlmnt-springboot.git
RUN git clone --branch ctrl https://github.com/bistrulli/acmeair-authservice-springboot.git

WORKDIR /root/acmeair-ctrlmnt-springboot
RUN mvn clean install

WORKDIR /root/acmeair-authservice-springboot
RUN mvn clean package

WORKDIR /root/acmeair-flightservice-springboot
RUN mvn clean package

EXPOSE 80
CMD ["java", "-jar", "/root/acmeair-flightservice-springboot/target/acmeair-flightservice-springboot-2.1.1-SNAPSHOT.jar", "--LICENSE=accept"]
