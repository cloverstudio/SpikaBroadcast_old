import React from "react";
import logo from "./logo.svg";
import "./App.scss";
import SpikaBroadcastWindow from "./components/spikabroadcast";
import { Link, useHistory } from "react-router-dom";

function Conference() {
  let history = useHistory();

  return (
    <div>
      <header></header>
      <main className="conference-main">
        <div className="peers">
          <div></div>
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />

          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />
          <div />

          <div />
        </div>
        <div className="controlls">
          <ul>
            <li>
              <a className="large_icon">
                <i className="fas fa-video"></i>
              </a>
            </li>
            <li>
              <a className="large_icon">
                <i className="fas fa-microphone"></i>
              </a>
            </li>
            <li>
              <a className="large_icon">
                <i className="fas fa-users"></i>
              </a>
            </li>
            <li>
              <a className="button">
                <i className="fal fa-arrow-up"></i>
              </a>
            </li>
            <li>
              <a className="button">end</a>
            </li>
          </ul>
        </div>
      </main>
      <footer></footer>
    </div>
  );
}

export default Conference;
