# building images
# docker build -t mediadriver -f MediaDriver/Dockerfile .
# start container
# If you use the --network=host option using these sysctls are not allowed.
# might want to increase shared memory size
docker run --shm-size=1g --ipc=shareable --name mediadriver mediadriver
# remove if need to rebuild
# docker container rm mediadriver
