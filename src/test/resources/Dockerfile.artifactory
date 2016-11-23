FROM java:8

VOLUME /tmp

RUN wget --user ${repo_auth_username} \
  --password ${repo_auth_password} \
  --content-disposition -O /app.jar \
  "${app_resource_url}"

RUN bash -c 'chmod a+rx /app.jar'

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
