
@echo off
chcp 65001 >nul
cd target\dist-client
java -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar mcp_financas_pessoais_client.jar "C:\Arquivos Teste"
cd ..\..
