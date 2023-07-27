### Example Project: Angular with [endpoint4s](https://endpoints4s.github.io/)

- A simple counter service is defined using endpoint4s. The example was copied from endpoint4s.
- The definition is shared by the server and the client.
- A Node module is generated that allows the Angular applicatioin to communicate with the server in a type safe manner. 

Generate the Node module that is used in the Angular application to access the server:

```
> sbt client/scalaTsFastOpt
```

Start the server via SBT:

```
> sbt server/run
```

In the webapp folder run:

```
> npm i
> npm start
```