export class Logger {
  private _output: boolean = false;
  private _prefix: string;

  constructor(prefix: string, output: boolean = false) {
    this._prefix = prefix;
    this._output = output;
  }

  debug(obj: any): void {
    if (!this._output) return;

    if (typeof obj === "string") console.log(`${this._prefix} debug: ${obj}`);
    else console.log(`${this._prefix} debug`, obj);
  }

  warn(obj: any): void {
    if (typeof obj === "string") console.log(`${this._prefix} warn: ${obj}`);
    else console.log(`${this._prefix} warn`, obj);
  }

  error(obj: any): void {
    if (typeof obj === "string") console.log(`${this._prefix} error: ${obj}`);
    else console.log(`${this._prefix} error`, obj);
  }
}
