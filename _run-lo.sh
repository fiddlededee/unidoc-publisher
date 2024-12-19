pushd "$(dirname "$0")/lo-docker" || exit
docker build -t lotest .
popd || exit
docker stop lo && docker rm lo
docker run --name lo -it -p 8101:8101 -v $PWD:/documents lotest soffice  --headless --nologo --nofirststartwizard --accept="socket,host=0.0.0.0,port=8101;urp"