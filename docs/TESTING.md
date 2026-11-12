# MyFit — Testes

## Automáticos

```bash
export JAVA_HOME=~/.local/share/jdks/jdk-21.0.13+9 ANDROID_HOME=~/Android/Sdk
./gradlew testDebugUnitTest      # unitários + Robolectric (domínio, banco, plataforma, UI)
./gradlew recordRoborazziDebug   # screenshots em app/build/outputs/roborazzi/
./gradlew lintDebug
./gradlew assembleRelease        # build com R8 (assinado se MYFIT_* estiver configurado)
```

O que cada camada cobre está em [ARCHITECTURE.md](ARCHITECTURE.md#testes).

## Roteiro manual no celular

O desenvolvimento foi feito sem dispositivo. Os itens abaixo validam o que só o aparelho prova: som, tela bloqueada, service, pasta real e o comportamento da One UI. Marque cada item; se algo falhar, anote o passo e o modelo do celular.

### 0. Instalação e permissões
- [ ] Instalar o APK (`adb install -r myfit-<versão>.apk` ou fluxo avançado, ver README).
- [ ] Abrir: tema escuro, cinco abas na barra inferior.
- [ ] *Ajustes → Bateria → Liberar*: marcar o MyFit como **Sem restrições**. O status passa a mostrar ✓.

### 1. Treinos
- [ ] Criar "Treino Costas" com 3 exercícios da biblioteca (busca "remada" sem acento funciona).
- [ ] Editar séries, repetições, descanso e carga digitando "42,5" (vírgula). Reordenar arrastando pela alça.
- [ ] Criar um exercício personalizado com imagem da galeria. Ela aparece na lista e no treino.
- [ ] Duplicar o treino (aparece "(cópia)") e excluir a cópia.

### 2. Execução (com o app aberto)
- [ ] Iniciar: na primeira vez o app pede permissão de notificações. Aceite.
- [ ] Concluir série → o descanso começa sozinho. Usar +15 s e −15 s.
- [ ] No fim do descanso: vibração, som e pulso verde; aparece "DESCANSO CONCLUÍDO".
- [ ] Ajustar carga com +/− e concluir: a próxima série herda a carga.
- [ ] Pausar (o tempo total para) e retomar. Pular série e pular exercício; tocar no exercício pulado na faixa superior para voltar a ele.

### 3. Tela bloqueada e segundo plano
- [ ] Concluir uma série e **bloquear a tela** durante o descanso: no fim, o celular toca e vibra, e a notificação mostra "Descanso concluído".
- [ ] Na tela de bloqueio, a notificação mostra o cronômetro e o botão "Pular descanso" funciona.
- [ ] Com o celular no **vibrar**: em *Ajustes → Som do fim do descanso → Toca sempre*, o alerta toca mesmo assim.
- [ ] Ícone de alarme na barra de status durante o descanso: é esperado (alarme exato).

### 4. Recuperação
- [ ] No meio do descanso, fechar o app pelos recentes (arrastar para fora). Reabrir: o Início mostra "Treino em andamento". "Retomar" volta na mesma série e o tempo total segue correto.
- [ ] Reiniciar o celular no meio do treino. Abrir o app e retomar: a sessão continua e o tempo total inclui o período do reboot.
- [ ] Finalizar o treino → resumo → manter "Atualizar cargas" → "Concluir e salvar".

### 5. Histórico
- [ ] O dia aparece marcado no calendário. Tocar no dia abre a sessão com séries, repetições e cargas.
- [ ] Alternar Semana/Mês nos resumos. Treinos, tempo total e frequência batem com o que foi feito.

### 6. Evolução
- [ ] Registrar duas avaliações em datas diferentes (editar a data no topo). Peso com vírgula.
- [ ] Comparar as duas: diferenças com sinal (+/−).
- [ ] Gráficos: peso e cintura aparecem; trocar indicadores e período.
- [ ] Fotos: adicionar duas (frente), selecionar ambas → "Comparar selecionadas"; excluir uma.
- [ ] O Início mostra peso, gordura e cintura com a variação.

### 7. Backup
- [ ] *Ajustes → Backup*: definir a senha de recuperação e guardá-la num gerenciador de senhas.
- [ ] Criar uma pasta (ex.: `MyFit Backup`) sincronizada com o Google Drive por um app de sincronização. Escolher essa pasta no MyFit.
- [ ] "Fazer backup agora": aparece um arquivo `myfit-backup-AAAAMMDD-HHmmss.myfit` na lista e no Drive depois da sincronização.
- [ ] Editar algo e esperar cerca de 10 min (ou o dia seguinte): um novo arquivo surge sozinho. No máximo 7 ficam na pasta.
- [ ] "Exportar backup para arquivo" (ex.: Downloads) funciona.
- [ ] **Restauração em instalação limpa**: desinstalar, reinstalar, *Ajustes → Backup → Restaurar de um arquivo* (ou escolher a pasta e tocar num backup) → confirmar a data → digitar a senha → treinos, histórico, medidas e fotos voltam.
- [ ] Senha errada mostra "Senha incorreta" e nada é alterado.
- [ ] Apagar a pasta escolhida e tocar "Fazer backup agora": o status mostra o erro em vermelho e chega uma notificação de falha.

### 8. Atualização
- [ ] Instalar uma versão nova por cima (`adb install -r`): os dados continuam.

## Problemas conhecidos
- O ícone de alarme aparece na barra durante o descanso (`setAlarmClock`, escolhido pela precisão).
- Com o app aberto, o fim do descanso também mostra a notificação no topo: é ela que toca o som.
- O seletor de pastas pode não listar o Google Drive diretamente. Use uma pasta local mantida por um app de sincronização.
