# Android build toolchain

O aplicativo Android nativo usa uma combinação fixa de ferramentas para manter o build local e automatizado reproduzível.

| Componente | Versão suportada |
| --- | --- |
| JDK usado para executar o Gradle | 17 |
| Gradle Wrapper | 8.7 |
| Android Gradle Plugin (AGP) | 8.6.1 |
| Kotlin Gradle plugins | 2.0.21 |
| Java source/target compatibility | 17 |
| Kotlin JVM target | 17 |

Use sempre o wrapper versionado no projeto. Uma instalação global do Gradle não é necessária.

## Terminal no Windows

Instale um JDK 17 e configure `JAVA_HOME` na sessão antes de executar o wrapper. Substitua o caminho abaixo pela instalação disponível na máquina:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Confirme que o Gradle está usando as versões esperadas:

```powershell
cd App\AndroidNative
.\gradlew.bat --version
```

A saída deve informar Gradle 8.7 e JVM 17. Alterar apenas `sourceCompatibility`, `targetCompatibility` ou `jvmTarget` não troca a JVM que executa o Gradle.

Para tornar a seleção permanente no Windows, configure `JAVA_HOME` nas variáveis de ambiente do usuário ou do agente de CI e inicie um novo terminal.

## Android Studio

No Android Studio, abra:

```text
Settings
  > Build, Execution, Deployment
  > Build Tools
  > Gradle
  > Gradle JDK
```

Selecione uma instalação JDK 17. A configuração do Android Studio é independente do `JAVA_HOME` usado por terminais já abertos; valide ambos quando os dois fluxos forem utilizados.

## CI e automação

Qualquer workflow que execute `App/AndroidNative/gradlew` deve instalar e selecionar JDK 17 antes do build. No momento desta formalização, o repositório não contém workflow de CI que invoque o wrapper Android. Ao adicionar um, fixe explicitamente a distribuição e a versão 17 do Java, em vez de depender do Java padrão do runner.

## Validação

Execute primeiro a identificação do ambiente e depois o build com todos os avisos habilitados:

```powershell
cd App\AndroidNative
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug --warning-mode all
```

O build deve terminar com `BUILD SUCCESSFUL` e gerar o APK de debug em `app\build\outputs\apk\debug\`. A saída e o relatório em `build\reports\problems\problems-report.html`, quando gerado, não devem conter:

- `IllegalArgumentException: 25.0.1` durante a configuração do KAPT;
- depreciações de `crunchPngs`;
- depreciações de `useProguard`;
- depreciações de `wearAppUnbundled`;
- depreciação de `StartParameter.isConfigurationCacheRequested`.

O configuration cache permanece habilitado em `gradle.properties`. Execute o build uma segunda vez e confirme que ele continua bem-sucedido e informa reutilização da entrada de cache quando nenhuma entrada relevante mudou.

## Diagnóstico

Se `gradlew --version` informar JVM 21, 25 ou outra versão, corrija primeiro o `JAVA_HOME` do terminal ou a opção **Gradle JDK** do Android Studio. Não implemente no projeto os getters sugeridos pelo relatório para `crunchPngs` ou `useProguard`: essas propriedades pertencem às classes internas do AGP, e os avisos são evitados usando a combinação de versões suportada acima.

Se resultados antigos persistirem após a troca do JDK, encerre os daemons antes de repetir a validação:

```powershell
.\gradlew.bat --stop
```
