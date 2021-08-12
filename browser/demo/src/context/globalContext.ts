import React, { Dispatch, SetStateAction } from "react";

export interface GlobalContextInterface {
  global: GlobalContextValuesInterface;
  setGlobal: Dispatch<SetStateAction<GlobalContextValuesInterface>>;
}

export interface GlobalContextValuesInterface {
  useCamera: boolean;
  useMicrophone: boolean;
  roomId?: String;
  peerId?: String;
}

const globalContextDeafultValues: GlobalContextValuesInterface = {
  useCamera: false,
  useMicrophone: false,
  roomId: "",
  peerId: "",
};

const GlobalContext = React.createContext<GlobalContextInterface>({
  global: globalContextDeafultValues,
  setGlobal: null,
});

export default GlobalContext;
export const defauiltValue = globalContextDeafultValues;
