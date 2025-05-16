#FROM quay.io/quarkus/quarkus-micro-image:1.0
FROM alpine
RUN apk add gcompat
WORKDIR /app
COPY j-vault /app/
EXPOSE 8005
CMD ./j-vault -V && ./j-vault auth-server --use-env true
