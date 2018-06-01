@echo off
echo Run VkArticleConverter for article: %1
java -jar "target/vk-article-converter-0.0-SNAPSHOT-jar-with-dependencies.jar" "C:\Users\Andrei Dashkevich\Documents\Ampersatd\runs\morocco-common-info.json" "C:\Users\Andrei Dashkevich\Documents\Ampersatd\converter.properties" %
pause