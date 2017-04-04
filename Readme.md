## Synopsis

Visualizing Erlang/Elixir processes as colored circles in the browser 

## Installation

Start the docker srevices:
```
./dcup
```
View logs:
```
./dclogs
```

Start clojure figwheel server 
(open code/frontend/index.html in browser before):
```     
./dcfigwheel
```

## Usage
- Drag an actor on canvas to start a process with that actor
- Click on an actor type (on the left) to open it's code
- Ctrl+S to save code. All running actors will have the new code (hot code load)