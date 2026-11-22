
@echo off
set MCP_FILES_ROOT="C:\Arquivos Teste"
cd target\dist-client
chcp 65001 >nul
java -Dstdin.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar mcp_financas_pessoais_client.jar
cd ..\..
set MCP_FILES_ROOT=
