FROM nginx:alpine

# Copy giao diện HTML
COPY index.html /usr/share/nginx/html/index.html

# Copy cấu hình Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]