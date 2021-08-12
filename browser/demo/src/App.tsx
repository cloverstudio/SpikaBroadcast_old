import React, { useState } from "react";
import logo from "./logo.svg";
import "./App.scss";
import SpikaBroadcastWindow from "./components/spikabroadcast";
import { BrowserRouter as Router, Switch, Route, Link } from "react-router-dom";

import Initial from "./Initial";
import Join from "./Join";
import Create from "./Create";
import Conference from "./Conference";
import GlobalContext, {
  defauiltValue,
  GlobalContextValuesInterface,
} from "./context/globalContext";

function App() {
  const [global, setGlobal] =
    useState<GlobalContextValuesInterface>(defauiltValue);

  return (
    <GlobalContext.Provider value={{ global, setGlobal }}>
      <Router>
        <Switch>
          <Route exact path="/">
            <Initial />
          </Route>
          <Route path="/join">
            <Join />
          </Route>
          <Route path="/create">
            <Create />
          </Route>
          <Route path="/conference/:roomId">
            <Conference />
          </Route>
        </Switch>
      </Router>
    </GlobalContext.Provider>
  );
}

export default App;
