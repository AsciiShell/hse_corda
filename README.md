# HSE CorDapp

В данной версии проекта реализован флоу для объединения и разделения 2х токенов, обмена между разными типами токенов. Всего 2 типа токенов, обменный коэффициент не изменяется и задан в коде. При выпуске токена можно задать его тип. Для каждой из операций с токеном реализованы проверки, например:
* объединение доступно только для токенов с одним владельцем и одного типа
* после каждой операции проверяется, что входное и выходное количество токенов каждого типа совпадают

Также реализован тест для контрактов, взятие комиссии при операции move.

### Собрать проект
```shell
./gradlew deployNodes
```

### Запустить ноды
```shell
./build/nodes/runnodes
```

## Работа в терминале

- Выпустить токен
```shell
flow start TokenIssueFlowInitiator owner: "PartyA", amount: 10, currencyType: RICK
flow start TokenIssueFlowInitiator owner: "PartyB", amount: 10, currencyType: MORTY
```

- Проверить токены текущей ноды
```
run vaultQuery contractStateType: com.exactpro.bootcamp.states.TokenState
```
- Разделить токен на 2 части
```
flow start TokenSplitFlowInitiator transactionId: "41AA642...", outputIndex: 0,  splitRatio: 0.7
```
- Объединить 2 токена
```shell
flow start TokenJoinFlowInitiator transactionId1: "41AA642...", outputIndex1: 1, transactionId2: "488A83...", outputIndex2: 1
```

- Выполнить обмен одного токена на другой
```shell
flow start TokenSwapFlowInitiator transactionId1: "A38D2F...", outputIndex1: 0, transactionId2: "E7A9AF...", outputIndex2: 0, needAmount: 3.0
```

# Проект выполнили
[Подчезерцев Алексей](https://github.com/AsciiShell),
[Самоделкина Мария](https://github.com/goo-goo-goo-joob),
[Солодянкин Андрей](https://github.com/andrsolo21), 2022
