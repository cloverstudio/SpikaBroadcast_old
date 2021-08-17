import React from "react";
import logo from "./logo.svg";
import "./App.scss";
import SpikaBroadcastWindow from "./components/spikabroadcast";
import { Link, useHistory } from "react-router-dom";

function PageInitial() {
  let history = useHistory();

  return (
    <div>
      <header></header>
      <main className="bg_color_gray">
        <div id="sub_main" className="bg_color_white radius_36 op_09">
          <ul className="buttons">
            <li>
              <div className="button type_01">
                <a onClick={(e) => history.push("/create")}>
                  <i className="fas fa-plus"></i>
                </a>
              </div>
              <p>
                <a>New meeting</a>
              </p>
            </li>
            <li>
              <div className="button type_02">
                <a onClick={(e) => history.push("/join")}>
                  <i className="fal fa-video"></i>
                </a>
              </div>
              <p>
                <a>Join a meeting</a>
              </p>
            </li>
          </ul>
        </div>
      </main>
      <footer></footer>
    </div>
  );
}

export default PageInitial;
