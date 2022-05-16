import sys
import argparse
import time
import quickfix
from application import Application

def main(config_file, script_file):
    """Main"""
    try:
        settings = quickfix.SessionSettings(config_file)
        application = Application()
        storefactory = quickfix.FileStoreFactory(settings)
        logfactory = quickfix.FileLogFactory(settings)
        initiator = quickfix.SocketInitiator(application, storefactory, settings, logfactory)

        initiator.start()
        time.sleep(1)
        if not script_file:
            # submit random orders
            application.run()
        else:
            with open(script_file) as file:
                for line in file:
                    if line.startswith('#'):
                        continue
                    order_command = line.strip().split(' ')
                    if order_command[2] == 'LIMIT':
                        assert len(order_command) == 6
                        side, symbol, _, qty, price, clord_id = order_command
                        application.put_new_order(symbol, side, 'LIMIT', float(qty), float(price), clord_id)
                    elif order_command[2] == 'MARKET':
                        assert len(order_command) == 5
                        side, symbol, _, qty, clord_id = order_command
                        application.put_new_order(symbol, side, 'MARKET', float(qty), None, clord_id)
                    elif order_command[2] == 'CANCEL':
                        assert len(order_command) == 5
                        side, symbol, _, origclord_id, clord_id = order_command
                        application.cancel_order(symbol, side, origclord_id, clord_id)
                    else:
                        print("Unknown order type")
                        sys.exit()
            while True:
                pass

    except (quickfix.ConfigError, quickfix.RuntimeError) as e:
        print(e)
        initiator.stop()
        sys.exit()

if __name__=='__main__':
    parser = argparse.ArgumentParser(description='FIX Client')
    parser.add_argument('config', type=str, help='configuration filename')
    parser.add_argument('-s', '--script', type=str, help='order script filename')
    args = parser.parse_args()
    main(args.config, args.script)
