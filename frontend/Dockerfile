FROM nginx:1.27-alpine
# A imagem oficial roda envsubst nos templates: so troca variaveis definidas (API_UPSTREAM).
ENV API_UPSTREAM=http://api:8080
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY . /usr/share/nginx/html
RUN rm -f /usr/share/nginx/html/Dockerfile /usr/share/nginx/html/nginx.conf
EXPOSE 80
