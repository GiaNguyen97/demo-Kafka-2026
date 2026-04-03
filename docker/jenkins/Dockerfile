FROM jenkins/jenkins:lts-jdk21

USER root

# Install prerequisites
RUN apt-get update && apt-get install -y \
    lsb-release \
    curl \
    gnupg \
    && rm -rf /var/lib/apt/lists/*

# Add Docker’s official GPG key
RUN curl -fsSLo /usr/share/keyrings/docker-archive-keyring.asc https://download.docker.com/linux/debian/gpg

# Set up the stable Docker repository
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.asc] https://download.docker.com/linux/debian $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list

# Install Docker CLI and Docker Compose Plugin
RUN apt-get update && apt-get install -y \
    docker-ce-cli \
    docker-compose-plugin \
    && rm -rf /var/lib/apt/lists/*
RUN ln -s /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

# Set standard permissions to jenkins user so it can execute pipelines
RUN usermod -aG root jenkins

# Drop back to jenkins user (If permission fails, advise user to run container as root or chmod sock)
USER jenkins
