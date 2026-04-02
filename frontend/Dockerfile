FROM nginx:alpine
# Copy toàn bộ file tĩnh (HTML, CSS, JS) vào thư mục mặc định của Nginx
COPY . /usr/share/nginx/html
# Expose port mặc định
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
