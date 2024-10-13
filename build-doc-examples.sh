./gradlew clean build publishToMavenLocal publish
# tag::body[]
echo build-doc
kotlin doc/build-doc.main.kts -h > doc/autopartials/example-cli-help.txt # <1>
rm -rf doc/output && mkdir doc/output
kotlin doc/build-doc.main.kts \
  --adoc-file doc/pages/unidoc-publisher-doc.adoc \
  --template approved/asciidoc/template-1.fodt \
  --fodt-output doc/output/unidoc-publisher-doc.fodt \
  --yaml-output doc/output/unidoc-publisher-doc.yaml \
  --html-output doc/output/index.html \
  --logo doc/images/unidoc-processor-symbol.svg \
  --check-spelling
cp doc/images -r doc-output
lo-kts-converter/lo-kts-converter.main.kts \
  -i doc/output/unidoc-publisher-doc.fodt -f pdf,odt,docx

echo ps-118
mvn install:install-file -Dfile=example/ps-118/JHyphenator-1.0.jar \
   -DgroupId=mfietz -DartifactId=jhyphenator -Dversion=1.0 -Dpackaging=jar # <2>
rm -rf example/ps-118/output && mkdir example/ps-118/output
kotlin example/ps-118/ps-118.main.kts
lo-kts-converter/lo-kts-converter.main.kts \
  -i example/ps-118/output/ps-118.fodt -f pdf,odt,docx
lo-kts-converter/lo-kts-converter.main.kts \
  -i example/ps-118/output/ps-118-ebook.fodt -f pdf

echo builder
rm -rf example/builder/output && mkdir example/builder/output
kotlin example/builder/table.main.kts && \
lo-kts-converter/lo-kts-converter.main.kts \
  -i example/builder/output/letter.fodt -f pdf,odt,docx

echo writerside-tutorial
rm -rf example/writerside-tutorial/output && mkdir example/writerside-tutorial/output
kotlin example/writerside-tutorial/writerside.main.kts
lo-kts-converter/lo-kts-converter.main.kts \
  -i example/writerside-tutorial/output/export-to-pdf.fodt -f pdf,odt,docx
# end::body[]

rm -rf doc-output
mkdir doc-output
cp doc/output/* doc-output
cp example/ps-118/output/* doc-output
cp example/builder/output/* doc-output
cp example/writerside-tutorial/output/* doc-output
zip doc-output.zip doc-output/*