FROM docker.m.daocloud.io/library/python:3.11-alpine

RUN addgroup -S judge && adduser -S -G judge judge

WORKDIR /app
COPY server.py /app/server.py

USER judge
EXPOSE 8099

ENTRYPOINT ["python", "-I", "-B", "/app/server.py"]
